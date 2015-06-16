/*
 * Licensed to Typesafe under one or more contributor license agreements.
 * See the AUTHORS file distributed with this work for
 * additional information regarding copyright ownership.
 * This file is licensed to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.squbs.unicomplex

import org.scalatest.{FlatSpecLike, Matchers}
import spray.http.Uri
import spray.http.Uri.Path
import spray.http.Uri.Path.{Empty, Segment, Slash}

class RegisterContextSpec extends FlatSpecLike with Matchers {


  "Path matching" should "work" in {
    val emptyPath = Path("")
    emptyPath shouldBe empty
    emptyPath should have length 0
    emptyPath.charCount should be (0)
    println(emptyPath.getClass.getName)
    emptyPath should be (Empty)
    emptyPath should not be 'startsWithSegment
    emptyPath should not be 'startsWithSlash
    emptyPath.startsWith(Empty) should be (true)


    val root = Path("/")
    root should not be empty
    root should have length 1
    root.charCount should be (1)
    println(root.getClass.getName)
    root shouldBe a [Slash]
    root should not be 'startsWithSegment
    root shouldBe 'startsWithSlash
    root.startsWith(Empty) should be (true)
    root.head should be ('/')
    root.tail should be (Empty)

    val single = Path("/abc")
    single should not be empty
    single should have length 2
    single.charCount should be (4)
    println(single.getClass.getName)
    single shouldBe a[Slash]
    single should not be 'startsWithSegment
    single shouldBe 'startsWithSlash
    single.startsWith(Path("/")) should be (true)
    single.startsWith(Path("")) should be (true)
    single.startsWith(Path("abc")) should be (false)
    single.head should be ('/')
    single.tail should be (Path("abc"))

    val simple = Path("abc")
    simple should not be empty
    simple should have length 1
    simple.charCount should be (3)
    println(simple.getClass.getName)
    simple shouldBe a[Segment]
    simple shouldBe 'startsWithSegment
    simple should not be 'startsWithSlash
    simple.startsWith(Path("/")) should be (false)
    simple.startsWith(Path("")) should be (true)
    simple.startsWith(Path("abc")) should be (true)
    simple.head should be ("abc")
    simple.tail should be (Empty)

    val multi = Path("abc/def")
    multi should not be empty
    multi should have length 3
    multi.charCount should be (7)
    println(multi.getClass.getName)
    multi shouldBe a[Segment]
    multi shouldBe 'startsWithSegment
    multi should not be 'startsWithSlash
    multi.startsWith(Path("/")) should be (false)
    multi.startsWith(Path("")) should be (true)
    multi.startsWith(Path("abc")) should be (true)
    multi.head should be ("abc")
    multi.tail shouldBe a [Slash]
    multi.startsWith(Path("abc/de")) should be (true)

  }

  "request path matching" should "work" in {

    Uri("http://www.ebay.com").path should not be 'startsWithSlash
    Uri("http://www.ebay.com").path should not be 'startsWithSegment
    Uri("http://www.ebay.com").path.startsWith(Empty) should be (true)
    Uri("http://www.ebay.com/").path shouldBe 'startsWithSlash
    Uri("http://www.ebay.com/").path should not be 'startsWithSegment
    Uri("http://www.ebay.com/").path.startsWith(Empty) should be (true)
    Uri("http://www.ebay.com").path should be (Path(""))
    Uri("http://www.ebay.com/").path should be (Path("/"))
    Uri("http://127.0.0.1:8080/abc").path shouldBe 'startsWithSlash
    Uri("http://www.ebay.com/").path.startsWith(Path("")) should be (true)
    Uri("http://www.ebay.com").path.startsWith(Path("")) should be (true)
    Uri("http://www.ebay.com/abc").path.startsWith(Path("")) should be (true)
    Uri("http://www.ebay.com/abc").path.tail.startsWith(Path("")) should be (true)
    Uri("http://www.ebay.com/abc").path.tail.startsWith(Path("abc")) should be (true)
    Uri("http://www.ebay.com/abc/def").path.tail.startsWith(Path("abc")) should be (true)
    Uri("http://www.ebay.com/abc/def").path.tail.startsWith(Path("abc/def")) should be (true)

  }

  "merge" should "work" in {
    import RegisterContext._

    var result = merge(Seq(), "abc", "old")
    result should have size 1
    result(0)._2 should be ("old")

    result = merge(result, "", "empty")
    result should have size 2
    result(0)._2 should be ("old")
    result(1)._2 should be ("empty")


    result = merge(result, "abc/def", "abc/def")
    result should have size 3
    result(0)._2 should be ("abc/def")
    result(1)._2 should be ("old")
    result(2)._2 should be ("empty")

    result = merge(result, "abc", "new")
    result should have size 3
    result(0)._2 should be ("abc/def")
    result(1)._2 should be ("new")
    result(2)._2 should be ("empty")

    var finding = result find {
      entry => pathMatch(Path("abc"), entry._1)
    }
    finding should not be None
    finding.get._2 should be ("new")

    finding = result find {
      entry => pathMatch(Path("abc/def"), entry._1)
    }
    finding should not be None
    finding.get._2 should be ("abc/def")

    finding = result find {
      entry => pathMatch(Path("aabc/def"), entry._1)
    }
    finding should not be None
    finding.get._2 should be ("empty")

    finding = result find {
      entry => pathMatch(Path("abc/defg"), entry._1)
    }
    finding should not be None
    finding.get._2 should be ("new")

    finding = result find {
      entry => pathMatch(Path("abcd/a"), entry._1) // abcd should not match either abc/def nor abc
    }
    finding.get._2 should be ("empty")
  }
}
