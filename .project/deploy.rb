require_relative "common/common"

def clojars_deploy()
  c = Common.new
  env = c.load_env
  cname = "#{env.namespace}-deploy"
  version = Time.now.utc.strftime("%Y%m%d")
  version_env = "REACT_CLJS_MODAL_VERSION=#{version}"
  clojars_password_env = "CLOJARS_PASSWORD=#{ENV["CLOJARS_PASSWORD"]}"
  c.run_inline %W{
    docker create --name #{cname}
    -w /w
    -v jars:/root/.m2
    -e #{version_env}
    -e #{clojars_password_env}
    clojure:lein-alpine
    lein deploy clojars
  }
  at_exit { c.run_inline %W{docker rm -f #{cname}} }
  env.source_file_paths.each do |src_path|
    c.pipe(
      %W{tar -c #{src_path}},
      %W{docker cp - #{cname}:/w}
    )
  end
  c.pipe(%W{tar -c .lein/profiles.clj}, %W{docker cp - #{cname}:/root})
  c.run_inline %W{docker start -a #{cname}}
end

Common.register_command({
  :invocation => "clojars-deploy",
  :description => "Deploys library to Clojars.",
  :fn => Proc.new { |*args| clojars_deploy(*args) }
})
