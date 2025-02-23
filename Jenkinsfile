#!groovy

pipeline {
  agent none
  // save some io during the build
  options { durabilityHint( 'PERFORMANCE_OPTIMIZED' ) }
  stages {
    stage("Parallel Stage") {
      parallel {
        stage( "Build Jdk8" ) {
          agent { node { label 'linux-light' } }
          steps {
            timeout( time: 120, unit: 'MINUTES' ) {
              withMaven( maven: 'maven3', jdk: 'jdk8' ) {
                sh "mvn -B -V clean install"
              }
            }
          }
        }
        stage( "Build Jdk11" ) {
          agent { node { label 'linux-light' } }
          steps {
            timeout( time: 120, unit: 'MINUTES' ) {
              withMaven( maven: 'maven3', jdk: 'jdk11' ) {
                sh "mvn -B -V clean install"
              }
            }
          }
        }
        stage( "Build Jdk17" ) {
          agent { node { label 'linux-light' } }
          steps {
            timeout( time: 120, unit: 'MINUTES' ) {
              withMaven( maven: 'maven3', jdk: 'jdk17' ) {
                sh "mvn -B -V clean install"
              }
            }
          }
        }
        stage( "Build Jdk21" ) {
          agent { node { label 'linux-light' } }
          steps {
            timeout( time: 120, unit: 'MINUTES' ) {
              withMaven( maven: 'maven3', jdk: 'jdk21' ) {
                sh "mvn -B -V clean install"
              }
            }
          }
        }
      }
    }
  }
}
// vim: et:ts=2:sw=2:ft=groovy
