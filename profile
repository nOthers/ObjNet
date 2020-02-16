BASH_DIRNAME="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export PYTHONPATH=${PYTHONPATH}:${BASH_DIRNAME}/SubProjects
