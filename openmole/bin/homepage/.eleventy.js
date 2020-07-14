module.exports = function(eleventyConfig) {
    /** filter `md` convert markdown to html
    * usage : {{ text | md | safe }}
    */
    eleventyConfig.addFilter("md", function(value) {
        let markdownIt = require("markdown-it");
        md = new markdownIt({
            html: true,
            linkify: true,
            typographer: true
          });
        return md.render(value)
    });

    /* filter `mdi`
    * usage : {{ text | mdi | safe }}
    * No surrounding <p> tag thanks to MarkdownIt `renderInline` 
    */
    eleventyConfig.addFilter("mdi", function(value) {
        let markdownIt = require("markdown-it");
        md = new markdownIt({
            html: true,
            linkify: true,
            typographer: true
          });
        return md.renderInline(value)
    });

    /* Copy the `assets/` directory */
    eleventyConfig.addPassthroughCopy("assets");
    return {
        dir: {
            output: "dist"
        },
        passthroughFileCopy: true      
    };
  };