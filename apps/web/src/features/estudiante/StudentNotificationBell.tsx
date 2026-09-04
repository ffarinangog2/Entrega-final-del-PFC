import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { obtenerLaboratorios, obtenerMaterias } from '../../services/academicoApi'
import { listarSesionesAbiertas, obtenerMiHorario, type Planificacion, type SesionAsistencia } from '../../services/operationalApi'

export function StudentNotificationBell() {
  const [sesiones,setSesiones]=useState<SesionAsistencia[]>([]); const [abierta,setAbierta]=useState(false)
  const [horario,setHorario]=useState<Planificacion[]>([]); const [materias,setMaterias]=useState<Map<string,string>>(new Map()); const [labs,setLabs]=useState<Map<string,string>>(new Map())
  useEffect(()=>{let activo=true;Promise.all([listarSesionesAbiertas(),obtenerMiHorario(),obtenerMaterias(),obtenerLaboratorios()]).then(([x,b,m,l])=>{if(activo){setSesiones(x);setHorario(b);setMaterias(new Map(m.map(item=>[item.id,item.nombre])));setLabs(new Map(l.map(item=>[item.id,item.codigo])))}}).catch(()=>undefined);return()=>{activo=false}},[])
  return <div className="student-bell"><button aria-label={`Notificaciones: ${sesiones.length} pendientes`} onClick={()=>setAbierta(x=>!x)}>🔔{sesiones.length>0&&<span>{sesiones.length}</span>}</button>
    {abierta&&<div role="dialog" aria-label="Notificaciones de asistencia">{sesiones.length===0?<p>No tienes notificaciones nuevas.</p>:sesiones.slice(0,5).map(s=>{const bloque=horario.find(item=>item.id===s.bloqueId);return <Link key={s.id} to="/asistencia">Asistencia habilitada · {materias.get(bloque?.materiaId??'')??'Actividad de laboratorio'} · {labs.get(bloque?.laboratorioId??'')??'Laboratorio'} · hasta {new Date(s.expiraEn).toLocaleTimeString([],{hour:'2-digit',minute:'2-digit'})}</Link>})}</div>}</div>
}
