require_relative "common/common"

def install(local)
  c = Common.new
  env = c.load_env
  cname = "#{env.namespace}-install"
  if local
    vol = "#{ENV["HOME"]}/.m2:/root/.m2"
  else
    vol = "jars:/root/.m2"
  end
  c.run_inline %W{
    docker create --name #{cname}
    -w /w
    -v #{vol}
    clojure
    lein install
  }
  at_exit { c.run_inline %W{docker rm -f #{cname}} }
  env.source_file_paths.each do |src_path|
    c.pipe(
      %W{tar -c #{src_path}},
      %W{docker cp - #{cname}:/w}
    )
  end
  c.run_inline %W{docker start -a #{cname}}
end

Common.register_command({
  :invocation => "local-install",
  :description => "Installs this jar into the local system repository (#{ENV["HOME"]}/.m2).",
  :fn => Proc.new { |*args| install(true, *args) }
})

Common.register_command({
  :invocation => "docker-install",
  :description => "Installs this jar in the docker \"jars\" volume.",
  :fn => Proc.new { |*args| install(false, *args) }
})
