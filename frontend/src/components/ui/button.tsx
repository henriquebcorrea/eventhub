import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

const buttonVariants = cva("focus-ring inline-flex min-h-11 items-center justify-center gap-2 rounded-xl px-5 text-sm font-bold transition disabled:pointer-events-none disabled:opacity-50", {
  variants: { variant: {
    primary: "bg-[var(--primary)] text-white shadow-[0_10px_24px_rgba(239,89,70,.25)] hover:bg-[var(--primary-strong)]",
    secondary: "border border-[var(--border)] bg-white text-[var(--navy)] hover:border-slate-400 hover:bg-slate-50",
    dark: "bg-[var(--navy)] text-white hover:bg-slate-700",
    ghost: "text-slate-700 hover:bg-slate-100",
  }, size: { default: "h-11", sm: "h-9 min-h-9 px-3", lg: "h-12 px-6 text-base" } },
  defaultVariants: { variant: "primary", size: "default" },
});

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement>, VariantProps<typeof buttonVariants> {}
export function Button({ className, variant, size, ...props }: ButtonProps) { return <button className={cn(buttonVariants({ variant, size }), className)} {...props} />; }
export { buttonVariants };

