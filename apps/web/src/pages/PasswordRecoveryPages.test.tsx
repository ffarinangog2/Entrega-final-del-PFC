import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ForgotPasswordPage } from './ForgotPasswordPage'
import { ResetPasswordPage } from './ResetPasswordPage'
import * as authApi from '../services/authApi'

vi.mock('../services/authApi', async () => {
  const actual = await vi.importActual<typeof import('../services/authApi')>('../services/authApi')
  return { ...actual, forgotPassword: vi.fn(), resetPassword: vi.fn() }
})
describe('recuperación de contraseña',()=>{
 beforeEach(()=>vi.resetAllMocks())
 it('muestra siempre la confirmación neutra',async()=>{vi.mocked(authApi.forgotPassword).mockResolvedValue({message:'ok'});render(<MemoryRouter><ForgotPasswordPage/></MemoryRouter>);fireEvent.change(screen.getByLabelText('Usuario o correo'),{target:{value:'usuario'}});fireEvent.click(screen.getByRole('button',{name:'Enviar instrucciones'}));expect(await screen.findByRole('status')).toHaveTextContent('Si la cuenta existe')})
 it('muestra requisitos y restablece con token',async()=>{vi.mocked(authApi.resetPassword).mockResolvedValue({message:'La contraseña se actualizó correctamente.'});render(<MemoryRouter initialEntries={['/restablecer-contrasena?token=abc']}><ResetPasswordPage/></MemoryRouter>);expect(screen.getByText(/Mínimo 12 caracteres/)).toBeInTheDocument();fireEvent.change(screen.getByLabelText('Nueva contraseña'),{target:{value:'ClaveNuevaSegura2!'}});fireEvent.change(screen.getByLabelText('Confirmar contraseña'),{target:{value:'ClaveNuevaSegura2!'}});fireEvent.click(screen.getByRole('button',{name:'Restablecer contraseña'}));await waitFor(()=>expect(authApi.resetPassword).toHaveBeenCalledWith('abc','ClaveNuevaSegura2!','ClaveNuevaSegura2!'))})
})
