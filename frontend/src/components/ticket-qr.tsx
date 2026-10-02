"use client";
import QRCode from "react-qr-code";
export function TicketQr({ value }: { value: string }) { return <div className="mx-auto w-fit border border-[var(--border)] bg-white p-4"><QRCode value={value} size={210} bgColor="#ffffff" fgColor="#0b0c12" level="M" /></div>; }

