import { FormEvent, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { AuthApiError, resetPassword } from '../services/authApi'
import './LoginPage.css'

export function ResetPasswordPage(){const [params]=useSearchParams();const token=params.get('token')||''
 const [password,setPassword]=useState('');const [confirm,setConfirm]=useState('');const [message,setMessage]=useState('');const [error,setError]=useState('')
 const checks=[['Mínimo 12 caracteres',password.length>=12],['Una mayúscula',/[A-Z]/.test(password)],['Una minúscula',/[a-z]/.test(password)],['Un número',/\d/.test(password)],['Un carácter especial',/[^A-Za-z0-9\s]/.test(password)]] as const
 async function submit(e:FormEvent){e.preventDefault();setError('');if(password!==confirm){setError('Las contraseñas no coinciden.');return}try{const r=await resetPassword(token,password,confirm);setMessage(r.message)}catch(err){setError(err instanceof AuthApiError?err.message:'No se pudo restablecer la contraseña.')}}
 return <main className="login-page"><section className="login-card"><h1>Nueva contraseña</h1><form className="login-form" onSubmit={submit}>
  <label htmlFor="new-password">Nueva contraseña</label><input id="new-password" type="password" required maxLength={64} value={password} onChange={e=>setPassword(e.target.value)}/>
  <label htmlFor="confirm-password">Confirmar contraseña</label><input id="confirm-password" type="password" required maxLength={64} value={confirm} onChange={e=>setConfirm(e.target.value)}/>
  <ul>{checks.map(([label,ok])=><li key={label}>{ok?'✓':'○'} {label}</li>)}</ul>{error&&<p role="alert">{error}</p>}{message&&<p role="status">{message}</p>}
  <button className="login-form__submit" disabled={!token}>Restablecer contraseña</button><Link to="/login">Volver al inicio de sesión</Link>
 </form></section></main>}
