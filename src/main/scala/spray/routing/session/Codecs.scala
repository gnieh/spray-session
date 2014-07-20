/*
* Copyright © 2014 spray-session
* Based on Play2.0 CookieBaker code.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package spray.routing.session

import scala.annotation.tailrec

/**
 * Utilities for Codecs operations.
 */
object Codecs {

  /**
   * Computes the SHA-1 digest for a byte array.
   *
   * @param bytes the data to hash
   * @return the SHA-1 digest, encoded as a hex string
   */
  def sha1(bytes: Array[Byte]): String = {
    import java.security.MessageDigest
    val digest = MessageDigest.getInstance("SHA-1")
    digest.reset()
    digest.update(bytes)
    digest.digest()
      .map(0xFF & _)
      .map { "%02x".format(_) }
      .foldLeft("") { _ + _ }
  }

  /**
   * Computes the MD5 digest for a byte array.
   *
   * @param bytes the data to hash
   * @return the MD5 digest, encoded as a hex string
   */
  def md5(bytes: Array[Byte]): String = {
    import java.security.MessageDigest
    val digest = MessageDigest.getInstance("MD5")
    digest.reset()
    digest.update(bytes)
    digest.digest()
      .map(0xFF & _)
      .map { "%02x".format(_) }
      .foldLeft("") { _ + _ }
  }

  /**
   * Compute the SHA-1 digest for a `String`.
   *
   * @param text the text to hash
   * @return the SHA-1 digest, encoded as a hex string
   */
  def sha1(text: String): String =
    sha1(text.getBytes)

  // --

  private val hexChars =
    Array('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')

  /**
   * Converts a byte array into an array of characters that denotes a hexadecimal representation.
   */
  def toHex(array: Array[Byte]): Array[Char] = {
    val result = new Array[Char](array.length * 2)
    @tailrec
    def loop(idx: Int): Unit =
      if(idx < array.length) {
        val b = array(idx) & 0xff
        result(2 * idx) = hexChars(b >> 4)
        result(2 * idx + 1) = hexChars(b & 0xf)
        loop(idx + 1)
      }
    loop(0)
    result
  }

  /**
   * Converts an array of characters that denotes a hexadecimal representation into a byte array
   */
  def fromHex(array: Array[Char]): Array[Byte] =
    if(array.length % 2 == 0) {
      val result = new Array[Byte](array.length / 2)
      @tailrec
      def loop(idx: Int): Unit =
        if(idx < array.length - 1) {
          val first = hexChars.indexOf(array(idx))
          val second = hexChars.indexOf(array(idx + 1))
          if(first == -1 || second == -1)
            throw new IllegalArgumentException("Input array is not a legal hexadecimal representation")
           result(idx / 2) = ((first << 4) + second).toByte
          loop(idx + 2)
        }
      loop(0)
      result
    } else {
      throw new IllegalArgumentException("Input array is not a legal hexadecimal representation")
    }

  /**
   * Converts a byte array into a `String` that denotes a hexadecimal representation.
   */
  def toHexString(array: Array[Byte]): String =
    new String(toHex(array))

  /**
   * Transform an hexadecimal String to a byte array.
   */
  def hexStringToByte(hexString: String): Array[Byte] =
    fromHex(hexString.toCharArray())

}
