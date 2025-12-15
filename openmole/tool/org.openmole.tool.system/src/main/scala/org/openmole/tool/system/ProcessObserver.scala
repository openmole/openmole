package org.openmole.tool.system

/*
 * Copyright (C) 2025 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.File
import squants.information.*

object ProcessObserver:

  def diskUsage(f: File): util.Try[Information] =
    import scala.sys.process.*
    import scala.util.*
    Try:
      val res = s"""du -ks ${f.getPath}""".!!.trim.takeWhile(_.isDigit)
      Kilobytes(res.toLong)

  def memory(pid: Long): util.Try[Information] =
    import scala.sys.process.*
    import scala.util.*

    val script =
      """
        |#!/usr/bin/env bash
        |# Usage: ./mem_tree.sh <PID>
        |# Reports total resident memory (RSS) of PID and its children, in kB
        |
        |pid=$1
        |[ -z "$pid" ] && { echo "Usage: $0 <PID>"; exit 1; }
        |
        |# Recursively collect PIDs
        |collect_pids() {
        |  local p=$1
        |  [ -r /proc/$p/stat ] || return
        |  echo $p
        |  for t in /proc/$p/task/*/children; do
        |    [ -r "$t" ] || continue
        |    for c in $(<"$t"); do collect_pids "$c"; done
        |  done
        |}
        |
        |# Sum RSS (resident set size, in kB) from /proc/[pid]/status
        |sum_rss() {
        |  for p in "$@"; do
        |    [ -r /proc/$p/status ] && awk '/VmRSS:/ {print $2}' /proc/$p/status
        |  done | awk '{s+=$1} END{print s+0}'
        |}
        |
        |# Collect and compute
        |pids=($(collect_pids $pid))
        |sum_rss "${pids[@]}"
        |""".stripMargin

    Try:
      val res = Seq("bash", "-c", script, "--", s"$pid").!!.trim
      Kilobytes(res.toLong)

  // 100 by core
  def cpuUsage(pid: Long, sleep: Int = 1): util.Try[Double] =
    import scala.sys.process.*
    import scala.util.*

    val script =
      """#!/usr/bin/env bash
        |# Usage: ./cpu_tree.sh <PID> [seconds]
        |# 100% = one full core
        |
        |pid=$1
        |dur=${2:-1}
        |[ -z "$pid" ] && { echo "Usage: $0 <PID> [seconds]"; exit 1; }
        |
        |clk_tck=$(getconf CLK_TCK)
        |LC_NUMERIC=C  # ensure dot decimal
        |
        |# Recursively collect PIDs
        |collect_pids() {
        |  local p=$1
        |  [ -r /proc/$p/stat ] || return
        |  echo $p
        |  for t in /proc/$p/task/*/children; do
        |    [ -r "$t" ] || continue
        |    for c in $(<"$t"); do collect_pids "$c"; done
        |  done
        |}
        |
        |# Sum utime+stime jiffies
        |sum_jiffies() {
        |  for p in "$@"; do
        |    [ -r /proc/$p/stat ] && awk '{
        |      sub(/^[0-9]+ \([^)]*\) /,"")
        |      print $(12)+$(13)
        |    }' /proc/$p/stat
        |  done | awk '{s+=$1} END{print s+0}'
        |}
        |
        |# First snapshot
        |pids=($(collect_pids $pid))
        |j0=$(sum_jiffies "${pids[@]}")
        |t0=$(date +%s%N)
        |
        |sleep "$dur"
        |
        |# Second snapshot
        |pids=($(collect_pids $pid))
        |if [ ${#pids[@]} -eq 0 ]; then
        |  j1=$j0   # process tree gone → assume no more CPU
        |else
        |  j1=$(sum_jiffies "${pids[@]}")
        |fi
        |t1=$(date +%s%N)
        |
        |dj=$((j1 - j0))
        |elapsed=$(awk -v ns=$((t1 - t0)) 'BEGIN{print ns/1e9}')
        |
        |awk -v dj="$dj" -v clk="$clk_tck" -v sec="$elapsed" \
        |  'BEGIN {
        |     usage = (dj / (clk * sec)) * 100
        |     if (usage < 0) usage = 0
        |     printf "%.2f\n", usage
        |   }'
        |""".stripMargin

    Try:
      val res = Seq("bash", "-c", script, "--", s"$pid", s"$sleep").!!.trim
      res.toDouble

