package ec.edu.scli.reservas.domain.state.reserva;

import ec.edu.scli.reservas.domain.model.EstadoReserva;

public final class ReservaStates {
    private static final ReservaState PROGRAMADA = new ProgramadaState();
    private static final ReservaState EN_CURSO = new EnCursoState();
    private static final ReservaState FINALIZADA = new FinalizadaState();
    private static final ReservaState CANCELADA = new CanceladaState();
    private static final ReservaState NO_ASISTIDA = new NoAsistidaState();

    private ReservaStates() { }

    public static ReservaState desde(EstadoReserva estado) {
        return switch (estado) {
            case PROGRAMADA -> PROGRAMADA;
            case EN_CURSO -> EN_CURSO;
            case FINALIZADA -> FINALIZADA;
            case CANCELADA -> CANCELADA;
            case NO_ASISTIDA -> NO_ASISTIDA;
        };
    }
}
