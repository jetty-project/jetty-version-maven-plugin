#!groovy

pipeline {
  agent any
  // save some io during the build
  options { durabilityHint( 'PERFORMANCE_OPTIMIZED' ) }
  stages {
    stage( "Build Jdk8" ) {
      agent { node { label 'linux' } }
      steps {
        timeout(time: 120, unit: 'MINUTES') {
          withMaven(maven:'maven3',jdk:'jdk8') {
            sh "mvn -B -V clean install"
          }
        }
      }
    }
  }
}
// vim: et:ts=2:sw=2:ft=groovy
