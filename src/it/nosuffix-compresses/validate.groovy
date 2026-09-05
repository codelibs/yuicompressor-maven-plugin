// Two regressions in one build, both silent and both ending in BUILD SUCCESS.
try {

  // 1. nosuffix=true without force. maven-resources-plugin copies the source to
  //    target/classes first, and with an empty suffix THAT copy is the mojo's
  //    output file - so "output is newer than input" fired on every file and the
  //    raw source shipped.
  def js = new File(basedir, "target/classes/js/app.js")
  assert js.exists() : "app.js was not produced at all"
  def text = js.text
  assert !text.contains("// a comment") : "the file was not compressed, it is the raw source: " + text
  assert !text.contains("var message") : "the file was not munged: " + text

  // 2. an extension with no compressor. The if/else-if chain had no else, so
  //    nothing was written and the empty temp file was moved over the destination.
  def jsx = new File(basedir, "target/classes/js/App.jsx")
  assert jsx.exists() : "App.jsx was not produced at all"
  assert jsx.length() > 0 : "App.jsx was written as 0 bytes - its content was destroyed"
  assert jsx.text == new File(basedir, "src/main/resources/js/App.jsx").text :
      "App.jsx should be copied unchanged, got: " + jsx.text

  return true

} catch(Throwable e) {
  e.printStackTrace()
  return false
}
