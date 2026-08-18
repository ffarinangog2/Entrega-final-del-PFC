package ec.edu.scli.usuarios.application.service;

import ec.edu.scli.usuarios.presentation.dto.perfil.PerfilAuthResponse;
import ec.edu.scli.usuarios.presentation.dto.perfil.PerfilCreateRequest;
import ec.edu.scli.usuarios.presentation.dto.perfil.PerfilExistsResponse;
import ec.edu.scli.usuarios.presentation.dto.perfil.PerfilResponse;
import ec.edu.scli.usuarios.presentation.dto.perfil.PerfilUpdateRequest;
import ec.edu.scli.usuarios.domain.model.Perfil;
import ec.edu.scli.usuarios.domain.exception.ConflictException;
import ec.edu.scli.usuarios.domain.exception.ResourceNotFoundException;
import ec.edu.scli.usuarios.domain.event.PerfilEvent;
import ec.edu.scli.usuarios.domain.event.PerfilEventListener;
import ec.edu.scli.usuarios.domain.port.AdministradorRepositoryPort;
import ec.edu.scli.usuarios.domain.port.DocenteRepositoryPort;
import ec.edu.scli.usuarios.domain.port.EstudianteRepositoryPort;
import ec.edu.scli.usuarios.domain.port.PerfilRepositoryPort;
import ec.edu.scli.usuarios.domain.port.TecnicoRepositoryPort;
import ec.edu.scli.usuarios.application.usecase.PerfilService;
import ec.edu.scli.usuarios.domain.model.TipoPerfil;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PerfilServiceImpl implements PerfilService {

        private final PerfilRepositoryPort perfilRepository;
        private final DocenteRepositoryPort docenteRepository;
        private final EstudianteRepositoryPort estudianteRepository;
        private final TecnicoRepositoryPort tecnicoRepository;
        private final AdministradorRepositoryPort administradorRepository;
        private final List<PerfilEventListener> eventListeners;

        public PerfilServiceImpl(
                        PerfilRepositoryPort perfilRepository,
                        DocenteRepositoryPort docenteRepository,
                        EstudianteRepositoryPort estudianteRepository,
                        TecnicoRepositoryPort tecnicoRepository,
                        AdministradorRepositoryPort administradorRepository,
                        List<PerfilEventListener> eventListeners) {
                this.perfilRepository = perfilRepository;
                this.docenteRepository = docenteRepository;
                this.estudianteRepository = estudianteRepository;
                this.tecnicoRepository = tecnicoRepository;
                this.administradorRepository = administradorRepository;
                this.eventListeners = eventListeners;
        }

        @Override
        @Transactional
        public PerfilResponse crear(PerfilCreateRequest request) {

                validarIdentificacionDuplicada(request.identificacion());

                validarEmailDuplicado(request.emailInstitucional());

                Perfil perfil = new Perfil();

                perfil.setIdentificacion(request.identificacion());
                perfil.setNombres(request.nombres());
                perfil.setApellidos(request.apellidos());
                perfil.setEmailInstitucional(
                                request.emailInstitucional().toLowerCase());
                perfil.setEmailPersonal(
                                normalizarEmail(request.emailPersonal()));
                perfil.setTelefono(request.telefono());
                perfil.setDireccion(request.direccion());
                perfil.setFechaNacimiento(request.fechaNacimiento());
                perfil.setFotoUrl(request.fotoUrl());
                perfil.setActivo(true);

                Perfil perfilGuardado = perfilRepository.save(perfil);

                publicarEvento(PerfilEvent.creado(perfilGuardado));

                return convertirAResponse(perfilGuardado);
        }

        @Override
        @Transactional(readOnly = true)
        public Page<PerfilResponse> listar(
                        String identificacion,
                        String nombre,
                        String email,
                        TipoPerfil tipoPerfil,
                        Boolean activo,
                        Pageable pageable) {
                return PageableMapper.toSpringPage(perfilRepository
                                .findAll(
                                                identificacion,
                                                nombre,
                                                email,
                                                tipoPerfil,
                                                activo,
                                                PageableMapper.toCriteria(pageable))
                                .map(this::convertirAResponse));
        }

        @Override
        @Transactional(readOnly = true)
        public PerfilResponse obtenerPorId(UUID id) {

                Perfil perfil = buscarPerfil(id);

                return convertirAResponse(perfil);
        }

        @Override
        @Transactional
        public PerfilResponse actualizar(
                        UUID id,
                        PerfilUpdateRequest request) {

                Perfil perfil = buscarPerfil(id);

                validarIdentificacionActualizacion(
                                perfil,
                                request.identificacion());

                validarEmailActualizacion(
                                perfil,
                                request.emailInstitucional());

                perfil.setIdentificacion(request.identificacion());
                perfil.setNombres(request.nombres());
                perfil.setApellidos(request.apellidos());
                perfil.setEmailInstitucional(
                                request.emailInstitucional().toLowerCase());
                perfil.setEmailPersonal(
                                normalizarEmail(request.emailPersonal()));
                perfil.setTelefono(request.telefono());
                perfil.setDireccion(request.direccion());
                perfil.setFechaNacimiento(request.fechaNacimiento());
                perfil.setFotoUrl(request.fotoUrl());

                Perfil perfilActualizado = perfilRepository.save(perfil);

                publicarEvento(PerfilEvent.estadoCambiado(perfilActualizado));

                return convertirAResponse(perfilActualizado);
        }

        @Override
        @Transactional
        public PerfilResponse cambiarEstado(
                        UUID id,
                        Boolean activo) {

                Perfil perfil = buscarPerfil(id);

                perfil.setActivo(activo);

                Perfil perfilActualizado = perfilRepository.save(perfil);

                return convertirAResponse(perfilActualizado);
        }

        @Override
        @Transactional
        public void eliminar(UUID id) {

                Perfil perfil = buscarPerfil(id);

                perfil.setActivo(false);

                Perfil perfilActualizado = perfilRepository.save(perfil);

                publicarEvento(PerfilEvent.estadoCambiado(perfilActualizado));
        }

        @Override
        @Transactional(readOnly = true)
        public PerfilExistsResponse verificarExistencia(UUID perfilId) {

                return perfilRepository
                                .findById(perfilId)
                                .map(perfil -> new PerfilExistsResponse(
                                                perfil.getId(),
                                                true,
                                                perfil.getActivo(),
                                                obtenerTiposPerfil(perfil.getId())))
                                .orElseGet(() -> new PerfilExistsResponse(
                                                perfilId,
                                                false,
                                                false,
                                                List.of()));
        }

        @Override
        @Transactional(readOnly = true)
        public PerfilAuthResponse obtenerParaAuth(UUID perfilId) {

                Perfil perfil = buscarPerfil(perfilId);

                return new PerfilAuthResponse(
                                perfil.getId(),
                                perfil.getNombres(),
                                perfil.getApellidos(),
                                perfil.getEmailInstitucional(),
                                perfil.getActivo(),
                                obtenerTiposPerfil(perfil.getId()));
        }

        private Perfil buscarPerfil(UUID id) {

                return perfilRepository
                                .findById(id)
                                .orElseThrow(
                                                () -> new ResourceNotFoundException(
                                                                "No existe un perfil con el id: " + id));
        }

        private void validarIdentificacionDuplicada(
                        String identificacion) {

                if (identificacion != null
                                && perfilRepository.existsByIdentificacion(identificacion)) {

                        throw new ConflictException(
                                        "Ya existe un perfil con la identificación: "
                                                        + identificacion);
                }
        }

        private void validarEmailDuplicado(String email) {

                String emailNormalizado = email.toLowerCase();

                if (perfilRepository
                                .existsByEmailInstitucional(emailNormalizado)) {

                        throw new ConflictException(
                                        "Ya existe un perfil con el email institucional: "
                                                        + emailNormalizado);
                }
        }

        private void validarIdentificacionActualizacion(
                        Perfil perfil,
                        String nuevaIdentificacion) {

                if (nuevaIdentificacion == null) {
                        return;
                }

                perfilRepository
                                .findByIdentificacion(nuevaIdentificacion)
                                .filter(encontrado -> !encontrado.getId().equals(perfil.getId()))
                                .ifPresent(encontrado -> {
                                        throw new ConflictException(
                                                        "Ya existe otro perfil con la identificación: "
                                                                        + nuevaIdentificacion);
                                });
        }

        private void validarEmailActualizacion(
                        Perfil perfil,
                        String nuevoEmail) {

                String emailNormalizado = nuevoEmail.toLowerCase();

                perfilRepository
                                .findByEmailInstitucional(emailNormalizado)
                                .filter(encontrado -> !encontrado.getId().equals(perfil.getId()))
                                .ifPresent(encontrado -> {
                                        throw new ConflictException(
                                                        "Ya existe otro perfil con el email institucional: "
                                                                        + emailNormalizado);
                                });
        }

        private String normalizarEmail(String email) {

                if (email == null || email.isBlank()) {
                        return null;
                }

                return email.toLowerCase();
        }

        private List<String> obtenerTiposPerfil(UUID perfilId) {

                List<String> tipos = new ArrayList<>();

                if (docenteRepository.existsByPerfilId(perfilId)) {
                        tipos.add("DOCENTE");
                }

                if (estudianteRepository.existsByPerfilId(perfilId)) {
                        tipos.add("ESTUDIANTE");
                }

                if (tecnicoRepository.existsByPerfilId(perfilId)) {
                        tipos.add("TECNICO");
                }

                if (administradorRepository.existsByPerfilId(perfilId)) {
                        tipos.add("ADMINISTRADOR");
                }

                return tipos;
        }

        private PerfilResponse convertirAResponse(Perfil perfil) {

                return new PerfilResponse(
                                perfil.getId(),
                                perfil.getIdentificacion(),
                                perfil.getNombres(),
                                perfil.getApellidos(),
                                perfil.getEmailInstitucional(),
                                perfil.getEmailPersonal(),
                                perfil.getTelefono(),
                                perfil.getDireccion(),
                                perfil.getFechaNacimiento(),
                                perfil.getFotoUrl(),
                                perfil.getActivo(),
                                perfil.getCreadoEn(),
                                perfil.getActualizadoEn());
        }

        private void publicarEvento(PerfilEvent event) {
                eventListeners.forEach(listener -> listener.onPerfilEvent(event));
        }
}
