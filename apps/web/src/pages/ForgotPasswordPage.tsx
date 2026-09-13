import { FormEvent, useState } from 'react'
import { Link } from 'react-router-dom'
import { forgotPassword } from '../services/authApi'
import './LoginPage.css'

const neutral = 'Si la cuenta existe, se enviaron instrucciones al correo registrado.'
export function ForgotPasswordPage() {
  const [identifier,setIdentifier]=useState(''); const [message,setMessage]=useState(''); const [loading,setLoading]=useState(false)
  async function submit(event:FormEvent){event.preventDefault();setLoading(true);try{await forgotPassword(identifier.trim())}catch{/* respuesta visual neutra */}finally{setMessage(neutral);setLoading(false)}}
  return <main className="login-page"><section className="login-card"><h1>Recuperar contraseña</h1>
    <form className="login-form" onSubmit={submit}><label htmlFor="recovery-id">Usuario o correo</label>
      <input id="recovery-id" required value={identifier} onChange={e=>setIdentifier(e.target.value)}/>
      <button className="login-form__submit" disabled={loading}>{loading?'Enviando...':'Enviar instrucciones'}</button>
      {message&&<p role="status">{message}</p>}<Link to="/login">Volver al inicio de sesión</Link>
    </form></section></main>
}
