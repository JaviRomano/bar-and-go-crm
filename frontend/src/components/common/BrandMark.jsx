import { useId } from 'react';

// Símbolo de Bar&Go: vaso de cerveza con espuma cuya pared derecha es una flecha. Colores de marca
// (--mark, --accent, --foam); el trazo usa --on-mark.
const BrandMark = ({ size = 32, title = 'Bar&Go' }) => {
  const clipId = useId();
  return (
    <svg width={size} height={size} viewBox="0 0 120 120" role="img" aria-label={title}>
      <defs>
        <clipPath id={clipId}><path d="M24 30 L76 30 L70 98 L30 98 Z" /></clipPath>
      </defs>
      <rect width="120" height="120" rx="27" style={{ fill: 'var(--mark)' }} />
      <g transform="translate(-0.5,-4)">
        <g clipPath={`url(#${clipId})`}>
          <rect x="16" y="52" width="84" height="50" style={{ fill: 'var(--accent)' }} />
          <path d="M18 40 L96 40 L96 56 L18 56 Z" style={{ fill: 'var(--foam)' }} />
        </g>
        <polygon points="85,26 101,64 77.5,102" style={{ fill: 'var(--on-mark)' }} />
        <path d="M24 30 L76 30 L70 98 L30 98 Z" fill="none" style={{ stroke: 'var(--on-mark)' }} strokeWidth={size <= 48 ? 8 : 6}
              strokeMiterlimit="10" />
      </g>
    </svg>
  );
};

export default BrandMark;
