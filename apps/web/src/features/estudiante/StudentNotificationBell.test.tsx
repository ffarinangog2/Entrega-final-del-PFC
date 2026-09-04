import { fireEvent,render,screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach,describe,expect,it,vi } from 'vitest'
import * as api from '../../services/operationalApi'
import * as academico from '../../services/academicoApi'
import { StudentNotificationBell } from './StudentNotificationBell'
vi.mock('../../services/operationalApi')
vi.mock('../../services/academicoApi')
describe('campana del estudiante',()=>{beforeEach(()=>{vi.resetAllMocks();vi.mocked(api.obtenerMiHorario).mockResolvedValue([]);vi.mocked(academico.obtenerMaterias).mockResolvedValue([]);vi.mocked(academico.obtenerLaboratorios).mockResolvedValue([])})
 it('muestra contador y acceso contextual a asistencia',async()=>{vi.mocked(api.listarSesionesAbiertas).mockResolvedValue([{id:'s',reservaId:null,bloqueId:'b',fechaClase:'2026-09-03',abiertaEn:'2026-09-03T14:00:00Z',expiraEn:'2026-09-03T14:15:00Z',estado:'ABIERTA',token:null}]);vi.mocked(api.obtenerMiHorario).mockResolvedValue([{id:'b',planificacionId:'p',nivel:7,periodoId:'pe',carreraId:'c',materiaId:'m',docenteId:'d',laboratorioId:'l',diaSemana:'LUNES',horaInicio:'14:00',horaFin:'16:00',estado:'CONFIRMADA',observacion:null,version:0}]);vi.mocked(academico.obtenerMaterias).mockResolvedValue([{id:'m',carreraId:'c',codigo:'MAT',nombre:'Aplicaciones',numeroHoras:2,nivel:7,activo:true}]);vi.mocked(academico.obtenerLaboratorios).mockResolvedValue([{id:'l',pisoId:'pi',codigo:'LAB-03',nombre:'Lab 3',capacidad:30,descripcion:'',estado:'DISPONIBLE',activo:true,creadoEn:'',actualizadoEn:''}]);render(<MemoryRouter><StudentNotificationBell/></MemoryRouter>);const button=await screen.findByRole('button',{name:'Notificaciones: 1 pendientes'});fireEvent.click(button);expect(screen.getByRole('link',{name:/Aplicaciones · LAB-03/})).toHaveAttribute('href','/asistencia')})
 it('muestra estado vacío sin fabricar notificaciones',async()=>{vi.mocked(api.listarSesionesAbiertas).mockResolvedValue([]);render(<MemoryRouter><StudentNotificationBell/></MemoryRouter>);const button=await screen.findByRole('button',{name:'Notificaciones: 0 pendientes'});fireEvent.click(button);expect(screen.getByText('No tienes notificaciones nuevas.')).toBeInTheDocument()})
})
