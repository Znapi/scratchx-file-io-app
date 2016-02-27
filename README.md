# Simple File I/O Helper App for Scratch Extension
This is a helper app to be used with the File I/O Scratch Extension.

See this project's pages [here](http://znapi.github.io/scratchx-file-io/) for more information and instructions to use it.

The extension (written in JavaScript) is located in the gh-pages branch with the project pages.

---

The helper app is a simple HTTP server that binds to and listens on port 8080. The port cannot be changed. This means that if you already have a service on port 8080, you will have to stop it. If the port was allowed to be changed, then the Scratch extension would have to ask for a port number, and it becomes a usability issue. I don't want to confuse Scratchers by asking them for a port number.

Credits
---
Uses [NanoHTTPD](http://nanohttpd.com).
