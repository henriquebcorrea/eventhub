import * as React from "react";
import { cn } from "@/lib/utils";
export function Input({ className, ...props }: React.InputHTMLAttributes<HTMLInputElement>) { return <input className={cn("focus-ring h-12 w-full rounded-sm border border-[var(--border)] bg-white px-4 text-base text-slate-900 placeholder:text-slate-500", className)} {...props} />; }

