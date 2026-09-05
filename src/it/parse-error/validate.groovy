// A compression that throws must not leave its ".tmp" scratch file behind.
// The build fails - that part is expected and is what invoker.buildResult
// records - but the half-written file used to survive into the output tree,
// where no later build removed it and the packaging plugins picked it up.
try {

  def leftovers = []
  ["target/classes", "target/parse-error"].each { dir ->
    def d = new File(basedir, dir)
    if (d.exists()) {
      d.eachFileRecurse { f -> if (f.isFile() && f.name.endsWith(".tmp")) leftovers << f }
    }
  }

  assert leftovers.isEmpty() : "compression failed and left scratch files behind: " + leftovers

  return true

} catch(Throwable e) {
  e.printStackTrace()
  return false
}
