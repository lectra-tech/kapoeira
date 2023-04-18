package com.lectra.kapoeira

import io.cucumber.junit.{Cucumber, CucumberOptions}
import org.junit.runner.RunWith

@RunWith(classOf[Cucumber])
@CucumberOptions(
  features = Array("classpath:features"),
  glue = Array("classpath:com.lectra.kapoeira.glue"),
  plugin = Array(
      "pretty",
      "json:target/reports/kapoeira-report.json",
      "junit:target/reports/kapoeira-report.xml",
      "html:target/reports/kapoeira-report.html")
)
class FeaturesTestRunner {}
