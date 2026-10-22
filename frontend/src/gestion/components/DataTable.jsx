import { Empty, ErrorNotice, Spinner } from './feedback';

/**
 * Tabla con columnas declarativas: { key, label, render?, align? }. Con loading muestra el spinner y
 * con error, el mensaje del backend.
 */
export const DataTable = ({ columns, rows, rowKey = 'id', loading, error, empty = 'Sin datos', onRowClick, footer }) => {
  if (loading) return <Spinner />;
  if (error) return <ErrorNotice error={error} />;
  if (!rows || rows.length === 0) return <Empty>{empty}</Empty>;
  return (
    <div className="g-table-wrap">
      <table className="g-table">
        <thead>
          <tr>
            {columns.map((col) => (
              <th key={col.key} className={col.align === 'right' ? 'g-num' : undefined}>
                {col.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, index) => (
            <tr
              key={typeof rowKey === 'function' ? rowKey(row, index) : row[rowKey]}
              className={onRowClick ? 'g-row--clickable' : undefined}
              onClick={onRowClick ? () => onRowClick(row) : undefined}
            >
              {columns.map((col) => (
                <td key={col.key} className={col.align === 'right' ? 'g-num' : undefined}>
                  {col.render ? col.render(row) : row[col.key] ?? '—'}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
        {footer && <tfoot>{footer}</tfoot>}
      </table>
    </div>
  );
};
