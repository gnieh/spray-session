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

import javax.crypto._
import javax.crypto.spec.SecretKeySpec

import com.typesafe.config.Config

case class CryptoException(val message: String) extends Throwable

/** Cryptographic utilities. */
class Crypto(config: Config) {

  private def secret =
    Option(config.getString("spray.routing.session.baker.secret"))

  /**
   * Signs the given String with HMAC-SHA1 using the given key.
   */
  def sign(message: String, key: Array[Byte]): String = {
    val mac = Mac.getInstance("HmacSHA1")
    mac.init(new SecretKeySpec(key, "HmacSHA1"))
    Codecs.toHexString(mac.doFinal(message.getBytes("utf-8")))
  }

  /**
   * Signs the given String with HMAC-SHA1 using the application’s secret key.
   */
  def sign(message: String): String =
    secret.fold(throw CryptoException("Configuration error: Missing `spray.routing.session.baker.secret`")) { secret =>
      sign(message, secret.getBytes("utf-8"))
    }

  /**
   * Encrypt a String with the AES encryption standard using the application secret
   * @param value The String to encrypt
   * @return An hexadecimal encrypted string
   */
  def encryptAES(value: String): String =
    secret.fold(throw CryptoException("Configuration error: Missing `spray.routing.session.baker.secret`")) { secret =>
      encryptAES(value, secret.substring(0, 16))
    }

  /**
   * Encrypt a String with the AES encryption standard. Private key must have a length of 16 bytes
   * @param value The String to encrypt
   * @param privateKey The key used to encrypt
   * @return An hexadecimal encrypted string
   */
  def encryptAES(value: String, privateKey: String): String = {
    val raw = privateKey.getBytes("utf-8")
    val skeySpec = new SecretKeySpec(raw, "AES")
    val cipher = Cipher.getInstance("AES")
    cipher.init(Cipher.ENCRYPT_MODE, skeySpec)
    Codecs.toHexString(cipher.doFinal(value.getBytes("utf-8")))
  }

  /**
   * Decrypt a String with the AES encryption standard using the application secret
   * @param value An hexadecimal encrypted string
   * @return The decrypted String
   */
  def decryptAES(value: String): String = {
    secret.map(secret => decryptAES(value, secret.substring(0, 16))).getOrElse {
      throw CryptoException("Configuration error: Missing application.secret")
    }
  }

  /**
   * Decrypt a String with the AES encryption standard. Private key must have a length of 16 bytes
   * @param value An hexadecimal encrypted string
   * @param privateKey The key used to encrypt
   * @return The decrypted String
   */
  def decryptAES(value: String, privateKey: String): String = {
    val raw = privateKey.getBytes("utf-8")
    val skeySpec = new SecretKeySpec(raw, "AES")
    val cipher = Cipher.getInstance("AES")
    cipher.init(Cipher.DECRYPT_MODE, skeySpec)
    new String(cipher.doFinal(Codecs.hexStringToByte(value)))
  }

}
