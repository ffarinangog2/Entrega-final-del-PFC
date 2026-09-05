import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { obtenerCarreras, obtenerLaboratorios, obtenerMaterias, obtenerPeriodos, obtenerPisos } from '../../services/academicoApi'
import { listarSesionesAbiertas, obtenerMiHorario, type Planificacion, type SesionAsistencia } from '../../services/operationalApi'
import { obtenerDocenteResumen, obtenerMiContextoAcademico } from '../../services/usuariosApi'

export function StudentHome() {
  const [data,setData]=useState<{carrera:string;nivel:number;ciclo:string;horario:Planificacion[];sesiones:SesionAsistencia[];materias:Map<string,string>;labs:Map<string,string>;ubicaciones:Map<string,number>;docentes:Map<string,string>}|null>(null)
  const [error,setError]=useState('')
  useEffect(()=>{let activo=true;Promise.all([obtenerMiContextoAcademico(),obtenerCarreras(),obtenerPeriodos(),obtenerMaterias(),obtenerLaboratorios(),listarSesionesAbiertas(),obtenerPisos()]).then(async ([ctx,carreras,periodos,materias,labs,sesiones,pisos])=>{
    const horario=await obtenerMiHorario(ctx.periodoId);const ids=[...new Set(horario.map(x=>x.docenteId).filter((id):id is string=>Boolean(id)))];const resumenes=await Promise.all(ids.map(id=>obtenerDocenteResumen(id).catch(()=>null)));const pisosPorId=new Map((pisos??[]).map(x=>[x.id,x.numero]));if(activo)setData({carrera:carreras.find(x=>x.id===ctx.carreraId)?.nombre??'Carrera institucional',nivel:ctx.nivel,ciclo:periodos.find(x=>x.id===ctx.periodoId)?.nombre??'Período académico',horario,sesiones,materias:new Map(materias.map(x=>[x.id,x.nombre])),labs:new Map(labs.map(x=>[x.id,x.codigo])),ubicaciones:new Map(labs.map(x=>[x.id,pisosPorId.get(x.pisoId)??0])),docentes:new Map(resumenes.filter(x=>x!==null).map(x=>[x.id,`${x.nombres} ${x.apellidos}`]))})
  }).catch(e=>activo&&setError(e instanceof Error?e.message:'No se pudo cargar tu información académica.'));return()=>{activo=false}},[])
  return <section className="role-home"><h1>Mi información académica</h1>{error&&<p role="alert">{error}</p>}{!data&&!error&&<p role="status">Cargando...</p>}{data&&<><section className="role-home__summary"><p><strong>Carrera:</strong> {data.carrera}</p><p><strong>Nivel:</strong> {data.nivel}</p><p><strong>Ciclo:</strong> {data.ciclo}</p></section>
    {data.sesiones.length>0&&<section className="role-home__summary"><h2>Clase en curso · Asistencia habilitada</h2><p>Tienes {data.sesiones.length} sesión disponible.</p><Link to="/asistencia">Registrar mi asistencia</Link></section>}
    <section><h2>Próximas clases</h2>{data.horario.length===0?<p>No existe un horario aprobado para tu contexto actual.</p>:data.horario.slice(0,3).map(b=>{const laboratorioId=b.laboratorioId;return <article key={b.id}><strong>{data.materias.get(b.materiaId)??'Materia'}</strong><span>{b.diaSemana} {b.horaInicio}–{b.horaFin} · {data.labs.get(laboratorioId)??'Laboratorio'}{data.ubicaciones.get(laboratorioId)?` · Piso ${data.ubicaciones.get(laboratorioId)}`:''}</span><span>{b.docenteId?data.docentes.get(b.docenteId)??'Docente asignado':'Docente por confirmar'}</span></article>})}</section>
    <div className="role-home__links"><Link to="/mi-horario">Ver mi horario</Link><Link to="/laboratorios">Laboratorios</Link><Link to="/historial">Historial</Link><Link to="/perfil">Mi perfil</Link></div></>}</section>
}
