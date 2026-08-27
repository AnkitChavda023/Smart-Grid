import * as d3 from 'd3'
import { useEffect, useRef } from 'react'
import type { OrderResponse } from '../../types'

interface GraphNode extends d3.SimulationNodeDatum {
  id: string
  kind: 'order' | 'vendor'
  label: string
  status?: string
}

interface GraphLink extends d3.SimulationLinkDatum<GraphNode> {
  id: string
}

const STATUS_COLOR: Record<string, string> = {
  PENDING: '#d97706',
  CONFIRMED: '#16a34a',
  CANCELLED: '#dc2626',
  CLOSED: '#64748b',
}

function buildGraph(orders: OrderResponse[]): { nodes: GraphNode[]; links: GraphLink[] } {
  const nodes: GraphNode[] = []
  const links: GraphLink[] = []
  const vendorSeen = new Set<string>()

  for (const order of orders) {
    nodes.push({ id: `order:${order.id}`, kind: 'order', label: order.id.slice(0, 8), status: order.status })

    if (order.vendorId) {
      const vendorNodeId = `vendor:${order.vendorId}`
      if (!vendorSeen.has(vendorNodeId)) {
        vendorSeen.add(vendorNodeId)
        nodes.push({ id: vendorNodeId, kind: 'vendor', label: order.vendorId.slice(0, 8) })
      }
      links.push({ id: `${order.id}-${order.vendorId}`, source: `order:${order.id}`, target: vendorNodeId })
    }
  }
  return { nodes, links }
}

export function OrderGraph({ orders, height = 420 }: { orders: OrderResponse[]; height?: number }) {
  const svgRef = useRef<SVGSVGElement | null>(null)
  const containerRef = useRef<HTMLDivElement | null>(null)
  const simulationRef = useRef<d3.Simulation<GraphNode, GraphLink> | null>(null)

  useEffect(() => {
    const svg = d3.select(svgRef.current)
    const container = containerRef.current
    if (!container) return
    const width = container.clientWidth || 640

    const { nodes, links } = buildGraph(orders)

    const previousNodes = simulationRef.current?.nodes() ?? []
    const previousById = new Map(previousNodes.map((n) => [n.id, n]))
    for (const node of nodes) {
      const previous = previousById.get(node.id)
      if (previous) {
        node.x = previous.x
        node.y = previous.y
        node.vx = previous.vx
        node.vy = previous.vy
      }
    }

    svg.attr('viewBox', `0 0 ${width} ${height}`)

    const linkSelection = svg
      .select<SVGGElement>('.links')
      .selectAll<SVGLineElement, GraphLink>('line')
      .data(links, (d) => d.id)
      .join(
        (enter) => enter.append('line').attr('stroke', 'var(--color-border)').attr('stroke-width', 1.5).attr('opacity', 0).call((e) => e.transition().duration(300).attr('opacity', 1)),
        (update) => update,
        (exit) => exit.transition().duration(200).attr('opacity', 0).remove(),
      )

    const nodeSelection = svg
      .select<SVGGElement>('.nodes')
      .selectAll<SVGGElement, GraphNode>('g.node')
      .data(nodes, (d) => d.id)
      .join(
        (enter) => {
          const g = enter.append('g').attr('class', 'node').attr('opacity', 0)
          g.append('circle')
          g.append('text')
          g.transition().duration(300).attr('opacity', 1)
          return g
        },
        (update) => update,
        (exit) => exit.transition().duration(200).attr('opacity', 0).remove(),
      )

    nodeSelection
      .select('circle')
      .attr('r', (d) => (d.kind === 'order' ? 10 : 14))
      .attr('fill', (d) => (d.kind === 'order' ? STATUS_COLOR[d.status ?? ''] ?? '#94a3b8' : 'var(--color-accent)'))
      .attr('stroke', 'var(--color-surface)')
      .attr('stroke-width', 2)

    nodeSelection
      .select('text')
      .text((d) => d.label)
      .attr('font-size', 10)
      .attr('fill', 'var(--color-text-muted)')
      .attr('text-anchor', 'middle')
      .attr('dy', (d) => (d.kind === 'order' ? 22 : 26))

    const simulation =
      simulationRef.current ??
      d3
        .forceSimulation<GraphNode>()
        .force('charge', d3.forceManyBody().strength(-180))
        .force('collide', d3.forceCollide(24))

    simulation.nodes(nodes)
    simulation
      .force('link', d3.forceLink<GraphNode, GraphLink>(links).id((d) => d.id).distance(70))
      .force('center', d3.forceCenter(width / 2, height / 2))
      .alpha(0.6)
      .restart()

    simulation.on('tick', () => {
      linkSelection
        .attr('x1', (d) => (d.source as GraphNode).x ?? 0)
        .attr('y1', (d) => (d.source as GraphNode).y ?? 0)
        .attr('x2', (d) => (d.target as GraphNode).x ?? 0)
        .attr('y2', (d) => (d.target as GraphNode).y ?? 0)

      nodeSelection.attr('transform', (d) => `translate(${d.x ?? 0}, ${d.y ?? 0})`)
    })

    simulationRef.current = simulation

    return () => {
      simulation.stop()
    }
  }, [orders, height])

  return (
    <div ref={containerRef} className="w-full">
      <svg ref={svgRef} width="100%" height={height}>
        <g className="links" />
        <g className="nodes" />
      </svg>
    </div>
  )
}
