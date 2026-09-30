import { cn } from "@/lib/utils";
export function Badge({ children, className }: { children: React.ReactNode; className?: string }) { return <span className={cn("inline-flex items-center rounded-full bg-teal-50 px-2.5 py-1 text-xs font-bold uppercase tracking-[.08em] text-teal-700", className)}>{children}</span>; }

