/** @type {import('tailwindcss').Config} */
export default {
  content: ['./src/**/*.{astro,html,js,jsx,md,mdx,svelte,ts,tsx,vue}'],
  theme: {
    extend: {
      colors: {
        mentor: {
          // Brand Manual Primary Background
          night: '#0B1220', // Noche (50% suggested proportion)
          // Brand Manual Primary Ribbon / Accents
          sky: '#38BDF8', // Cielo (12% suggested proportion)
          indigo: '#6366F1', // Índigo (8% suggested proportion)
          // Brand Manual Secondary & Accent
          turquoise: '#2DD4BF', // Turquesa (5% suggested proportion)
          'deep-turquoise': '#0D9488', // Turquesa profundo (accent on light backgrounds)
          // Brand Manual Backgrounds & Surfaces
          ice: '#E6F4F8', // Hielo (25% suggested proportion)
          white: '#FFFFFF', // Blanco (cards, panels, and text)
          // Extended UI helpers
          violet: '#A78BFA',
          amber: '#FBBF24',
        },
      },
      backgroundImage: {
        // Brand Manual Degradado Cinta (#38BDF8 -> #6366F1)
        'gradient-ribbon': 'linear-gradient(135deg, #38BDF8 0%, #6366F1 100%)',
        // Brand Manual Degradado Ruta (#2DD4BF -> #38BDF8)
        'gradient-route': 'linear-gradient(135deg, #2DD4BF 0%, #38BDF8 100%)',
      },
      fontFamily: {
        // Brand Manual Headings: Poppins (Bold 700, Medium 500, Regular 400)
        heading: ['Poppins', 'sans-serif'],
        // Brand Manual Body & Interface: Inter (Bold 700, Medium 500, Regular 400)
        sans: ['Inter', 'sans-serif'],
      },
      fontSize: {
        // Brand Manual Typography Scale
        'display': ['48px', { lineHeight: '1.1', letterSpacing: '-0.03em' }],
        'heading-title': ['28px', { lineHeight: '1.25', letterSpacing: '-0.02em' }],
        'body-base': ['16px', { lineHeight: '1.5', letterSpacing: '0' }],
        'caption': ['12px', { lineHeight: '1.4', letterSpacing: '0.14em' }],
      },
      screens: {
        // Brand Manual Reference Resolutions
        'mobile-sm': '390px',
        'mobile-lg': '414px',
        'tablet': '768px',
        'desktop': '1280px',
      },
      spacing: {
        '1': '4px',
        '2': '8px',
        '4': '16px',
        '6': '24px',
        '8': '32px',
        '12': '48px',
      },
      borderRadius: {
        sm: '4px',
        md: '8px',
        lg: '12px',
        xl: '16px',
        '2xl': '20px',
        '3xl': '24px',
        full: '9999px',
      },
    },
  },
  plugins: [],
};
