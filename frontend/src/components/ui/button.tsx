import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

const buttonVariants = cva("focus-ring inline-flex min-h-11 items-center justify-center gap-2 rounded-sm px-5 text-sm font-extrabold transition disabled:pointer-events-none disabled:opacity-50", {
  variants: { variant: {
    primary: "bg-[var(--accent)] text-[var(--navy)] hover:bg-[#dcffa0]",
    secondary: "border border-[var(--border)] bg-[var(--surface)] text-[var(--navy)] hover:border-[var(--navy)] hover:bg-white",
    dark: "bg-[var(--navy)] text-white hover:bg-[#2a2c35]",
    ghost: "text-inherit hover:bg-current/10",
  }, size: { default: "h-11", sm: "h-9 min-h-9 px-3", lg: "h-12 px-6 text-base" } },
  defaultVariants: { variant: "primary", size: "default" },
});

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement>, VariantProps<typeof buttonVariants> {}
export function Button({ className, variant, size, ...props }: ButtonProps) { return <button className={cn(buttonVariants({ variant, size }), className)} {...props} />; }
export { buttonVariants };

