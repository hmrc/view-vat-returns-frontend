/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package utils

import java.util.Calendar

import com.google.inject.Inject
import com.kenshoo.play.metrics.Metrics

class MetricsReporter @Inject()(metrics: Metrics) {
  def incrementCounter(counterName: String): Unit = {
    metrics.defaultRegistry.counter(counterName).inc()
  }

  def currentMins(name: String): Unit = {
    val cal = Calendar.getInstance()
    val minsCounter = metrics.defaultRegistry.counter(name)
    if(minsCounter.getCount > 0) {
      minsCounter.dec(minsCounter.getCount())
    }
    minsCounter.inc(cal.get(Calendar.MINUTE))
  }
}
