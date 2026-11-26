/** @type {import('tailwindcss').Config} */
export default {
  content: ['./src/**/*.{astro,html,js,jsx,md,mdx,svelte,ts,tsx,vue}'],
  theme: {
    extend: {
      colors: {
        mentor: {
          night: '#0B1220',
          sky: '#38BDF8',
          indigo: '#6366F1',
          turquoise: '#2DD4BF',
          'deep-turquoise': '#0D9488',
          ice: '#E6F4F8',
          white: '#FFFFFF',
          violet: '#A78BFA',
          amber: '#FBBF24',
        },
      },
      backgroundImage: {
        'gradient-ribbon': 'linear-gradient(135deg, #38BDF8 0%, #6366F1 100%)',
        'gradient-route': 'linear-gradient(135deg, #2DD4BF 0%, #38BDF8 100%)',
      },
      fontFamily: {
        heading: ['Poppins', 'sans-serif'],
        sans: ['Inter', 'sans-serif'],
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
        full: '9999px',
      },
    },
  },
  plugins: [],
};
