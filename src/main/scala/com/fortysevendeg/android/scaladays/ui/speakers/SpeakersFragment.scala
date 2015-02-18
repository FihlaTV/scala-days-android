/*
 * Copyright (C) 2015 47 Degrees, LLC http://47deg.com hello@47deg.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may
 *  not use this file except in compliance with the License. You may obtain
 *  a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.fortysevendeg.android.scaladays.ui.speakers

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.{LayoutInflater, View, ViewGroup}
import com.fortysevendeg.android.scaladays.model.Speaker
import com.fortysevendeg.android.scaladays.modules.ComponentRegistryImpl
import com.fortysevendeg.android.scaladays.modules.json.JsonRequest
import com.fortysevendeg.android.scaladays.modules.net.NetRequest
import com.fortysevendeg.android.scaladays.ui.commons.{UiServices, LineItemDecorator}
import macroid.{Ui, AppContext, Contexts}
import scala.concurrent.ExecutionContext.Implicits.global
import macroid.FullDsl._
import com.fortysevendeg.macroid.extras.ViewTweaks._
import com.fortysevendeg.macroid.extras.RecyclerViewTweaks._
import com.fortysevendeg.macroid.extras.ActionsExtras._

class SpeakersFragment
  extends Fragment
  with Contexts[Fragment]
  with ComponentRegistryImpl
  with UiServices {

  override implicit lazy val appContextProvider: AppContext = fragmentAppContext

  private var fragmentLayout: Option[Layout] = None

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    val fLayout = new Layout
    fragmentLayout = Some(fLayout)
    runUi(
      (fLayout.recyclerView
        <~ rvLayoutManager(new LinearLayoutManager(appContextProvider.get))
        <~ rvAddItemDecoration(new LineItemDecorator())) ~
        (fLayout.reloadButton <~ On.click(Ui {
          loadSpeakers()
        })))
    fLayout.content
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
    super.onViewCreated(view, savedInstanceState)
    loadSpeakers()
  }

  def loadSpeakers(): Unit = {
    loading()
    val result = for {
      conference <- loadSelectedConference()
    } yield reloadList(conference.speakers)

    result recover {
      case _ => failed()
    }
  }

  def reloadList(speakers: Seq[Speaker]) = {
    speakers.length match {
      case 0 => empty()
      case _ =>
        for {
          layout <- fragmentLayout
          recyclerView <- layout.recyclerView
        } yield {
          val adapter = new SpeakersAdapter(speakers, new RecyclerClickListener {
            override def onClick(speaker: Speaker): Unit = {
              speaker.twitter map {
                twitterName =>
                  val intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/%s".format(twitterName)))
                  startActivity(intent)
              }

            }
          })
          runUi(
            (layout.progressBar <~ vGone) ~
              (layout.recyclerView <~ vVisible) ~
              (layout.recyclerView <~ rvAdapter(adapter))
          )
        }
    }
  }

  def loading() = {
    fragmentLayout map {
      layout =>
        runUi(
          (layout.progressBar <~ vVisible) ~
            (layout.recyclerView <~ vGone) ~
            (layout.placeholderContent <~ vGone))
    }
  }

  def failed() = {
    fragmentLayout map {
      layout =>
        layout.loadFailed()
        runUi(
          (layout.progressBar <~ vGone) ~
            (layout.recyclerView <~ vGone) ~
            (layout.placeholderContent <~ vVisible))
    }
  }

  def empty() = {
    fragmentLayout map {
      layout =>
        layout.loadEmpty()
        runUi(
          (layout.progressBar <~ vGone) ~
            (layout.recyclerView <~ vGone) ~
            (layout.placeholderContent <~ vVisible))
    }
  }

}
