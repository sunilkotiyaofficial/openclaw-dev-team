/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  // Only activate `dark:` variants when class="dark" is set explicitly.
  // Prevents auto-flipping based on OS preference, which was causing
  // half-dark-half-light styling chaos.
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        // Custom theme tokens via CSS variables — flip values for dark mode
        primary: 'rgb(var(--primary) / <alpha-value>)',
        accent: 'rgb(var(--accent) / <alpha-value>)',
        surface: 'rgb(var(--surface) / <alpha-value>)',
        ink: 'rgb(var(--ink) / <alpha-value>)',
      },
    },
  },
  plugins: [],
};
