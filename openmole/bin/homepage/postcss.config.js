const postcssAutoprefixer = require("autoprefixer");
const postcssCustomProperties = require("postcss-custom-properties");
const postcssMqpacker = require("css-mqpacker");
const postcssImport = require("postcss-import");
const postcssNANO = require("cssnano");

module.exports = {
  plugins: [
    postcssImport(),
    postcssAutoprefixer(),
    postcssCustomProperties(),
    // postcssMqpacker(),
    postcssNANO({
      preset: "default",
    }),
  ],
};
