import { ErrorNotice, Spinner } from '../../components';

// Mientras un informe carga, el spinner; si falla, el mensaje del backend.
const ReportState = ({ state }) => (state.loading ? <Spinner /> : <ErrorNotice error={state.error} />);

export default ReportState;
