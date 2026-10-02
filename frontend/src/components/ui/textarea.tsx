import * as React from "react";
import { cn } from "@/lib/utils";
export function Textarea({ className, ...props }: React.TextareaHTMLAttributes<HTMLTextAreaElement>) { return <textarea className={cn("focus-ring min-h-32 w-full resize-y rounded-sm border border-[var(--border)] bg-white px-4 py-3 text-base", className)} {...props} />; }

