"use client";
import QRCode from "react-qr-code";
export function TicketQr({ value }: { value: string }) { return <div className="mx-auto w-fit rounded-2xl bg-white p-4"><QRCode value={value} size={210} bgColor="#ffffff" fgColor="#111827" level="M" /></div>; }

