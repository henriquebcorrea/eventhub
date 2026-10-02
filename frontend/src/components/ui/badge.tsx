import { cn } from "@/lib/utils";
export function Badge({ children, className }: { children: React.ReactNode; className?: string }) { return <span className={cn("inline-flex items-center rounded-sm bg-[var(--accent)] px-2.5 py-1 text-[.65rem] font-extrabold uppercase tracking-[.11em] text-[var(--navy)]", className)}>{children}</span>; }

