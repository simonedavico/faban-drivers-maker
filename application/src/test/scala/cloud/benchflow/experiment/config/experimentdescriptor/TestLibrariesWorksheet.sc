import com.github.zafarkhaja.semver.Version

import scala.util.{Failure, Success, Try}

val v1 = Version.valueOf("7.5.0")
val v2 = Version.valueOf("7.3.0")

Try(Version.valueOf("v2")) match {
  case Success(semver) => println("successful")
  case Failure(ex) => println("not a semver")
}

val singleVersion = "([0-9]+\\.[0-9]+\\.[0-9]+.*)".r
val rangedVersionPattern =
  s"$singleVersion-$singleVersion".r

"7.3.0-rc+build1-7.5.0-rc-build.2" match {
  case rangedVersionPattern(low, high) =>
    println(low, high)
  case singleVersion(version) =>
    println(version)
}

val singlePort = "([0-9]{1,5})".r
val onlyPorts = s"$singlePort:$singlePort".r
val ipAndSinglePort = s"([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}):$singlePort".r
val ipAndPorts = s"$ipAndSinglePort:$singlePort".r


"127.0.0.1:3306:3306" match {
  case ipAndSinglePort(ip, port) =>
    println(ip, port)
  case ipAndPorts(ip, public, priv) =>
    println(ip, public, priv)
  case _ => println("diomadonna")
}

"127.0.0.1:3306" match {
  case ipAndSinglePort(ip, port) =>
    println(ip, port)
  case ipAndPorts(ip, public, priv) =>
    println(ip, public, priv)
  case _ => println("diomadonna")
}

"3306" match {
  case singlePort(port) => println(port)
  case ipAndSinglePort(ip, port) =>
    println(ip, port)
  case ipAndPorts(ip, public, priv) =>
    println(ip, public, priv)
  case _ => println("diomadonna")
}

"3306:3306" match {
  case onlyPorts(pub, priv) => println(pub, priv)
  case singlePort(port) => println(port)
  case ipAndSinglePort(ip, port) =>
    println(ip, port)
  case ipAndPorts(ip, public, priv) =>
    println(ip, public, priv)
  case _ => println("diomadonna")
}


