require_relative "common/common"

def clojars_deploy_version(version)
  c = Common.new
  env = c.load_env
  cname = "#{env.namespace}-deploy"
  version_env = "REACT_CLJS_MODAL_VERSION=#{version}"
  clojars_password_env = "CLOJARS_PASSWORD=#{ENV["CLOJARS_PASSWORD"]}"
  c.run_inline %W{
    docker create --name #{cname}
    -w /w
    -v jars:/root/.m2
    -e #{version_env}
    -e #{clojars_password_env}
    dmohs/clojurescript
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

def clojars_deploy()
  version = Time.now.utc.strftime("%Y.%m.%d")
  clojars_deploy_version(version)
  c.run_inline %W{git tag #{version}}
  c.run_inline %W{git push origin #{version}}
end

def clojars_deploy_via_travis(snapshot=nil)
  c = Common.new
  version = ENV["TRAVIS_TAG"].empty? ? ENV["TRAVIS_BRANCH"] : ENV["TRAVIS_TAG"]
  if snapshot == "snapshot"
    version = "#{version}-SNAPSHOT"
  end
  clojars_deploy_version version
end

Common.register_command({
  :invocation => "clojars-deploy",
  :description => "Deploys library to Clojars.",
  :fn => Proc.new { |*args| clojars_deploy(*args) }
})

Common.register_command({
  :invocation => "clojars-deploy-via-travis",
  :description => "Deploys library to Clojars (called by Travis CI).",
  :fn => Proc.new { |*args| clojars_deploy_via_travis(*args) }
})
