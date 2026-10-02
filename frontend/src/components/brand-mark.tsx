export function BrandMark({ className = "size-10" }: { className?: string }) {
  return <svg aria-hidden="true" className={className} viewBox="0 0 52 52" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M5 8H47V18C42.6 18 39 21.6 39 26S42.6 34 47 34V44H5V34C9.4 34 13 30.4 13 26S9.4 18 5 18V8Z" fill="currentColor" />
    <path d="M24 14V38M18 21L24 15L30 21M18 31L24 37L30 31" stroke="var(--navy)" strokeWidth="2.5" strokeLinecap="square" strokeLinejoin="miter" />
    <path d="M35 14V18M35 23V29M35 34V38" stroke="var(--navy)" strokeWidth="2" strokeLinecap="square" />
  </svg>;
}
