ace.define("ace/mode/openmole",["require","exports","module","ace/lib/oop"], function(require, exports, module) {
"use strict";
var oop = require("ace/lib/oop");

var ScalaMode = require("ace/mode/scala").Mode;
var OpenMOLEHighlightRules = require("ace/mode/openmole_highlight_rules").OpenMOLEHighlightRules;
var MatchingBraceOutdent = require("./matching_brace_outdent").MatchingBraceOutdent;


var Mode = function() {
    this.HighlightRules = OpenMOLEHighlightRules;
    this.$outdent = new MatchingBraceOutdent();

};
oop.inherits(Mode, ScalaMode);

exports.Mode = Mode;
});



ace.define('ace/mode/openmole_highlight_rules', ["require","exports","module","ace/lib/oop"], function(require, exports, module) {

var oop = require("ace/lib/oop");
var ScalaHighlightRules = require("ace/mode/scala_highlight_rules").ScalaHighlightRules;


var OpenMOLEHighlightRules = function() {


    var keywords = (
            "case|default|do|else|for|if|match|while|throw|return|try|trye|catch|finally|yield|" +
            "abstract|class|def|extends|final|forSome|implicit|implicits|import|lazy|new|object|null|" +
            "override|package|private|protected|sealed|super|this|trait|type|val|var|with|" +
            "assert|assume|require|print|println|"+
            ##OMKeywords##
    );

    var buildinConstants = ("true|false");

    var langClasses = (
        "Long|Integer|Short|Byte|Double|Number|Float|"+
        "Character|Boolean|"+
        "Iterable|"+
        "Class|String|Object|" +
        "Unit|Any|AnyVal|AnyRef|Seq|Iterable|List|" +
        "Option|Array|Char|Byte|Int|Long|Nothing|" +
        "BigDecimal|BigInt|Either|" +
        "Function|IndexedSeq|Iterator|" +
        "Map|" +
        "Range|Seq|Set|Stream|" +
        "Vector|File" +

        ##OMClasses##
    );

var keywordMapper = this.createKeywordMapper({
                            "variable.language": "this",
                            "keyword": keywords,
                            "support.function": langClasses,
                            "constant.language": buildinConstants
                        }, "identifier");

    this.$rules = {
        "start" : [
            {
                token : "comment",
                regex : "\\/\\/.*$"
            },
            {
                token : "comment", // multi line comment
                regex : "\\/\\*",
                next : "comment"
            }, {
                token : "string.regexp",
                regex : "[/](?:(?:\\[(?:\\\\]|[^\\]])+\\])|(?:\\\\/|[^\\]/]))*[/]\\w*\\s*(?=[).,;]|$)"
            }, {
                token : "string",
                regex : '"""',
                next : "tstring"
            }, {
                token : "string",
                regex : '"(?=.)', // " strings can't span multiple lines
                next : "string"
            }, {
                token : "symbol.constant", // single line
                regex : "'[\\w\\d_]+"
            }, {
                token : "constant.numeric", // hex
                regex : "0[xX][0-9a-fA-F]+\\b"
            }, {
                token : "constant.numeric", // float
                regex : "[+-]?\\d+(?:(?:\\.\\d*)?(?:[eE][+-]?\\d+)?)?\\b"
            }, {
                token : "constant.language.boolean",
                regex : "(?:true|false)\\b"
            }, {
                token : keywordMapper,
                // TODO: Unicode escape sequences
                // TODO: Unicode identifiers
                regex : "[a-zA-Z_$][a-zA-Z0-9_$]*\\b"
            }, {
                token : "keyword.operator",
                regex : "!|\\$|%|&|\\*|\\-\\-|\\-|\\+\\+|\\+|~|===|==|=|!=|!==|<=|>=|<<=|>>=|>>>=|<>|<|>|!|&&|\\|\\||\\?\\:|\\*=|%=|\\+=|\\-=|&=|\\^=|\\b(?:in|instanceof|new|delete|typeof|void)"
            }, {
                token : "paren.lparen",
                regex : "[[({]"
            }, {
                token : "paren.rparen",
                regex : "[\\])}]"
            }, {
                token : "text",
                regex : "\\s+"
            }
        ],
        "comment" : [
            {
                token : "comment", // closing comment
                regex : ".*?\\*\\/",
                next : "start"
            }, {
                token : "comment", // comment spanning whole line
                regex : ".+"
            }
        ],
        "string" : [
            {
                token : "escape",
                regex : '\\\\"'
            }, {
                token : "string",
                regex : '"',
                next : "start"
            }, {
                token : "string.invalid",
                regex : '[^"\\\\]*$',
                next : "start"
            }, {
                token : "string",
                regex : '[^"\\\\]+'
            }
        ],
        "tstring" : [
            {
                token : "string",
                regex : '"{3,5}',
                next : "start"
            }, {
                defaultToken : "string"
            }
        ]
    };
};

oop.inherits(OpenMOLEHighlightRules, ScalaHighlightRules);
exports.OpenMOLEHighlightRules = OpenMOLEHighlightRules;

});
(function() {
    ace.require(["ace/mode/openmole"], function(m) {
        if (typeof module == "object" && typeof exports == "object" && module) {
            module.exports = m;
        }
    });
})();

