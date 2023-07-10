const path = require('path');

module.exports = {
  "entry": {
    "openmole": ["##openmoleJS##"]
  },
  "context": path.resolve(__dirname, "##webuiDir##"),
  "output": {
    "path": "##bundleOutputDir##",
    "filename": "##bundleName##",
    "library": "openmole_library",
    "libraryTarget": "var"
  },
  "mode": "development",
  "devServer": {
    "port": 8080
  },
  "devtool":"source-map",
  "module": {
    "rules": [{
      "test": new RegExp("\\.js$"),
      "enforce": "pre",
      "use": ["source-map-loader"]
    }]
  }
}