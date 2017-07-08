var page = require('webpage').create();
var system = require('system');

if (system.args.length != 3) {
  console.log("Please provide the following arguments: url, output_dir");
  phantom.exit(1);
}

var address = system.args[1];
var output_dir = system.args[2];

page.viewportSize = { width: 1440, height: 900 };
page.open(address, function() {
  page.render(output_dir);
  phantom.exit();
});