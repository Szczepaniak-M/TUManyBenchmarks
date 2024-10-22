/** @type {import("tailwindcss").Config} */
module.exports = {
  content: [
    "./src/**/*.{html,ts}",
  ],
  theme: {
    extend: {
      container: {
        screens: {
          '2xl': '1740px', // Override 1536px with 1740px
        },
      },
    }
  },
  plugins: [],
  safelist: [
    "bg-gray-200",
  ],
}

