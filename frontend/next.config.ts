import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",
  allowedDevOrigins: ["127.0.0.1"],
  devIndicators: false,
  experimental: {
    typedEnv: true,
  },
};

export default nextConfig;

