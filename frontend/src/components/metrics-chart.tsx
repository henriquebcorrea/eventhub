"use client";
import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis } from "recharts";
import type { Metrics } from "@/lib/types";
export function MetricsChart({ data }: { data: Metrics["registrationsByDay"] }) {
  const formatted = data.map((item) => ({ ...item, label: new Intl.DateTimeFormat("pt-BR", { weekday: "short", day: "2-digit" }).format(new Date(item.day)) }));
  return <div className="h-64 w-full"><ResponsiveContainer><AreaChart data={formatted} margin={{ top: 10, right: 8, left: -20, bottom: 0 }}><defs><linearGradient id="eventhub-chart" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor="#e44c34" stopOpacity={.24}/><stop offset="100%" stopColor="#e44c34" stopOpacity={0}/></linearGradient></defs><CartesianGrid strokeDasharray="3 3" stroke="#d8d2c5" vertical={false}/><XAxis dataKey="label" axisLine={false} tickLine={false} tick={{ fill: "#6f706f", fontSize: 12 }}/><Tooltip contentStyle={{ borderRadius: 2, border: "1px solid #d8d2c5" }}/><Area type="monotone" dataKey="count" name="Inscrições" stroke="#e44c34" strokeWidth={3} fill="url(#eventhub-chart)" /></AreaChart></ResponsiveContainer></div>;
}
