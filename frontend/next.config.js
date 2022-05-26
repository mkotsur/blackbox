/** @type {import('next').NextConfig} */

require('dotenv').config()

const nextConfig = {
    reactStrictMode: true,
    rewrites: async () => [
        {
            source: '/api/:path*',
            destination: process.env.BB_BACKEND_URL + '/:path*',
        },
    ],
}

module.exports = nextConfig
