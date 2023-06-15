#!/usr/bin/env bash

export SH_DIR=$(dirname $0)
export KAPOEIRA_SH_DIR=$(dirname $(dirname ${SH_DIR}))

addHeader() {
  local file=$1
  local firstLineWithoutComment=$(grep -n -v '\*' "${file}"  | head -1 | cut -d : -f1)
  # drop old header
  if [[ ${firstLineWithoutComment} -ge 2 ]]; then
    sed -i "1,$((firstLineWithoutComment-1))d" "${file}"
  fi
  # add new header
  mv ${file} ${file}_bck
  cat "${SH_DIR}/scala-header-comment.txt" ${file}_bck > ${file}
  # clean
  rm ${file}_bck
}
export -f addHeader

find ${KAPOEIRA_SH_DIR} -type f -name "*.scala" -exec bash -c "addHeader '{}'" \;
