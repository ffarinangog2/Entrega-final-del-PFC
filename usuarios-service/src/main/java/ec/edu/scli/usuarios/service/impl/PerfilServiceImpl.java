package ec.edu.scli.usuarios.service.impl;

import ec.edu.scli.usuarios.dto.perfil.PerfilAuthResponse;
import ec.edu.scli.usuarios.dto.perfil.PerfilCreateRequest;
import ec.edu.scli.usuarios.dto.perfil.PerfilExistsResponse;
import ec.edu.scli.usuarios.dto.perfil.PerfilResponse;
import ec.edu.scli.usuarios.dto.perfil.PerfilUpdateRequest;
import ec.edu.scli.usuarios.entity.Perfil;
import ec.edu.scli.usuarios.exception.ConflictException;
import ec.edu.scli.usuarios.exception.ResourceNotFoundException;
import ec.edu.scli.usuarios.repository.AdministradorRepository;
import ec.edu.scli.usuarios.repository.DocenteRepository;
import ec.edu.scli.usuarios.repository.EstudianteRepository;
import ec.edu.scli.usuarios.repository.PerfilRepository;
import ec.edu.scli.usuarios.repository.TecnicoRepository;
import ec.edu.scli.usuarios.service.PerfilService;
import ec.edu.scli.usuarios.specification.PerfilSpecification;
import ec.edu.scli.usuarios.enums.TipoPerfil;

import org.springframework.data.jpa.domain.Specification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PerfilServiceImpl implements PerfilService {

        private final PerfilRepository perfilRepository;
        private final DocenteRepository docenteRepository;
        private final EstudianteRepository estudianteRepository;
        private final TecnicoRepository tecnicoRepository;
        private final AdministradorRepository administradorRepository;

        public PerfilServiceImpl(
                        PerfilRepository perfilRepository,
                        DocenteRepository docenteRepository,
                        EstudianteRepository estudianteRepository,
                        TecnicoRepository tecnicoRepository,
                        AdministradorRepository administradorRepository) {
                this.perfilRepository = perfilRepository;
                this.docenteRepository = docenteRepository;
                this.estudianteRepository = estudianteRepository;
                this.tecnicoRepository = tecnicoRepository;
                this.administradorRepository = administradorRepository;
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
                Specification<Perfil> specification = PerfilSpecification
                                .identificacionContiene(identificacion)
                                .and(
                                                PerfilSpecification
                                                                .nombreContiene(nombre))
                                .and(
                                                PerfilSpecification
                                                                .emailContiene(email))
                                .and(
                                                PerfilSpecification
                                                                .tieneTipoPerfil(tipoPerfil))
                                .and(
                                                PerfilSpecification
                                                                .tieneEstado(activo));

                return perfilRepository
                                .findAll(specification, pageable)
                                .map(this::convertirAResponse);
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

                perfilRepository.save(perfil);
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
}