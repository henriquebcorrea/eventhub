export function OrganizerHeading({ eyebrow, title, description }: { eyebrow: string; title: string; description: string }) {
  return <div className="poster-grid relative overflow-hidden bg-[var(--navy)] p-6 text-[var(--paper)] sm:p-9"><div className="relative"><p className="eyebrow text-[var(--accent)]">{eyebrow}</p><h1 className="display-tight mt-3 max-w-4xl text-4xl font-extrabold sm:text-5xl">{title}<span className="text-[var(--primary)]">.</span></h1><p className="mt-4 max-w-2xl text-sm text-white/65 sm:text-base">{description}</p></div></div>;
}
